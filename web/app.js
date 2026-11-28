(function(){
  var KEY="daily-todo-v1";
  var state={tasks:{}};
  function clean(p){
    var out={};
    if(!p||typeof p!=="object"||!p.tasks||typeof p.tasks!=="object")return out;
    Object.keys(p.tasks).forEach(function(d){
      if(!/^\d{4}-\d{2}-\d{2}$/.test(d)||!Array.isArray(p.tasks[d]))return;
      var list=[];
      p.tasks[d].slice(0,2000).forEach(function(t){
        if(!t||typeof t.text!=="string")return;
        var n={id:String(t.id||"").slice(0,40)||uid(),text:t.text.slice(0,200),done:t.done===true,
          pri:(t.pri==="high"||t.pri==="low")?t.pri:"med",created:Number(t.created)||0,doneAt:Number(t.doneAt)||null};
        if(t.hidden==="deleted"||t.hidden==="cleared")n.hidden=t.hidden;
        list.push(n);
      });
      out[d]=list;
    });
    return out;
  }
  try{var raw=localStorage.getItem(KEY); if(raw){state.tasks=clean(JSON.parse(raw));}}catch(e){}
  var db=null, userId=null, queue={};
  function setStatus(t){var el=document.getElementById("sync");if(el)el.textContent=t;}
  function saveLocal(){try{localStorage.setItem(KEY,JSON.stringify(state));}catch(e){}}
  function writeDay(day){
    if(!db||!userId)return;
    var body={tasks:state.tasks[day]||[]};
    queue[day]=(queue[day]||Promise.resolve()).then(function(){
      return db.collection("data/users/"+userId).doc("d-"+day).set(body);
    }).then(function(){setStatus("Saved to your account");})
      .catch(function(){setStatus("Couldn’t save to your account. Your tasks are kept on this device.");});
  }
  function save(day){saveLocal();writeDay(day||cur);}

  function fmt(d){var m=d.getMonth()+1,day=d.getDate();return d.getFullYear()+"-"+(m<10?"0":"")+m+"-"+(day<10?"0":"")+day;}
  function parse(s){var a=s.split("-");return new Date(+a[0],+a[1]-1,+a[2]);}
  function shift(s,n){var d=parse(s);d.setDate(d.getDate()+n);return fmt(d);}
  var todayStr=fmt(new Date());
  var cur=todayStr, filter="all", editing=null;
  var $=function(id){return document.getElementById(id);};

  var fx={enter:null,pop:null,slide:null,celebrate:false,histIn:false};
  var REDUCED=!!(window.matchMedia&&window.matchMedia("(prefers-reduced-motion: reduce)").matches);
  function celebrate(){
    var card=document.querySelector(".progress"), bar=$("bar");
    if(card){card.classList.remove("glow");void card.offsetWidth;card.classList.add("glow");}
    if(REDUCED)return;
    var r=bar.getBoundingClientRect(), cx=r.left+r.width/2, cy=r.top+r.height/2;
    var colors=["#0e7466","#3cc4ae","#facc15","#fb923c","#7ab4f0","#c2410c"];
    for(var i=0;i<30;i++){
      var e=document.createElement("span"); e.className="conf";
      var a=Math.random()*Math.PI*2, d=80+Math.random()*160;
      e.style.left=cx+"px"; e.style.top=cy+"px";
      e.style.setProperty("--c",colors[i%colors.length]);
      e.style.setProperty("--dx",Math.round(Math.cos(a)*d)+"px");
      e.style.setProperty("--dy",Math.round(Math.sin(a)*d+60)+"px");
      e.style.setProperty("--rot",Math.round(Math.random()*720-360)+"deg");
      document.body.appendChild(e);
      setTimeout((function(el){return function(){el.remove();};})(e),1400);
    }
  }
  function slideTo(day){fx.slide=day<cur?"r":day>cur?"l":null;}
  function tasks(){return state.tasks[cur]||(state.tasks[cur]=[]);}
  function uid(){return Date.now().toString(36)+Math.random().toString(36).slice(2,6);}

  function render(){
    var d=parse(cur);
    $("dateTitle").textContent=d.toLocaleDateString(undefined,{weekday:"long"});
    var rel=cur===todayStr?"Today · ":cur===shift(todayStr,-1)?"Yesterday · ":cur===shift(todayStr,1)?"Tomorrow · ":"";
    $("dateSub").textContent=rel.replace(" · ",", ")+d.toLocaleDateString(undefined,{month:"long",day:"numeric",year:"numeric"});
    $("pick").value=cur;

    var all=tasks().filter(function(t){return !t.hidden;}), done=all.filter(function(t){return t.done;}).length;
    var pct=all.length?Math.round(done/all.length*100):0;
    $("pText").textContent=done+" of "+all.length+" done";
    $("pPct").textContent=pct+"%";
    $("fill").style.width=pct+"%";
    $("bar").setAttribute("aria-valuenow",pct);

    var order={high:0,med:1,low:2};
    var shown=all.filter(function(t){return filter==="all"||(filter==="done"?t.done:!t.done);})
      .sort(function(a,b){return (a.done-b.done)||(order[a.pri]-order[b.pri]);});

    var ul=$("list"); ul.textContent=""; ul.className=""; if(fx.slide&&!REDUCED){void ul.offsetWidth;ul.className="slide-"+fx.slide;} fx.slide=null;
    if(!shown.length){
      var e=document.createElement("div"); e.className="empty";
      e.textContent=all.length?"Nothing here with this filter.":"No tasks yet. Add your first task above.";
      ul.appendChild(e);
    }
    shown.forEach(function(t){
      var li=document.createElement("li"); li.className="task p-"+t.pri+(t.done?" done":"")+(fx.enter===t.id?" enter":"")+(fx.pop===t.id?" pop":"");
      var cb=document.createElement("input"); cb.type="checkbox"; cb.checked=t.done; cb.setAttribute("aria-label","Mark done: "+t.text);
      cb.onchange=function(){
        t.done=cb.checked;t.doneAt=cb.checked?Date.now():null;fx.pop=t.id;
        var vis=tasks().filter(function(x){return !x.hidden;});
        if(cb.checked&&vis.length&&vis.every(function(x){return x.done;}))fx.celebrate=true;
        save();render();
      };
      li.appendChild(cb);
      if(editing===t.id){
        var inp=document.createElement("input"); inp.className="edit"; inp.value=t.text; inp.maxLength=200;
        var commit=function(){var v=inp.value.trim(); if(v)t.text=v; editing=null; save(); render();};
        inp.onkeydown=function(ev){if(ev.key==="Enter")commit(); if(ev.key==="Escape"){editing=null;render();}};
        inp.onblur=commit;
        li.appendChild(inp);
        setTimeout(function(){inp.focus();inp.select();},0);
      }else{
        var b=document.createElement("button"); b.className="txt"; b.textContent=t.text; b.title="Edit task";
        b.onclick=function(){editing=t.id;render();};
        li.appendChild(b);
      }
      var del=document.createElement("button"); del.className="del"; del.textContent="×"; del.setAttribute("aria-label","Delete task: "+t.text);
      del.onclick=function(){li.classList.add("leave");setTimeout(function(){t.hidden="deleted";save();render();},REDUCED?0:220);};
      li.appendChild(del);
      ul.appendChild(li);
    });

    fx.enter=null;fx.pop=null;
    if(fx.celebrate){fx.celebrate=false;celebrate();}
    var prevOpen=(state.tasks[shift(cur,-1)]||[]).filter(function(t){return !t.done&&!t.hidden;}).length;
    $("carry").style.display=prevOpen?"":"none";
    $("carry").textContent="Move "+prevOpen+" unfinished "+(prevOpen===1?"task":"tasks")+" from the day before";
    $("clearDone").style.display=done?"":"none";
    if(document.body.classList.contains("hist"))renderHist();
  }

  function add(){
    var v=$("newText").value.trim(); if(!v){$("newText").focus();return;}
    var nid=uid(); fx.enter=nid; tasks().push({id:nid,text:v,done:false,pri:$("newPri").value,created:Date.now()});
    $("newText").value=""; save(); render(); $("newText").focus();
  }
  $("addBtn").onclick=add;
  $("newText").onkeydown=function(e){if(e.key==="Enter")add();};
  $("prev").onclick=function(){fx.slide="r";cur=shift(cur,-1);editing=null;render();};
  $("next").onclick=function(){fx.slide="l";cur=shift(cur,1);editing=null;render();};
  $("today").onclick=function(){slideTo(todayStr);cur=todayStr;editing=null;render();};
  function openPicker(){var p=$("pick");try{if(p.showPicker){p.showPicker();return;}}catch(e){}p.focus();}
  $("dateTitle").onclick=openPicker; $("dateSub").onclick=openPicker;
  $("dateTitle").onkeydown=function(e){if(e.key==="Enter"||e.key===" "){e.preventDefault();openPicker();}};
  $("pick").onchange=function(e){if(e.target.value){slideTo(e.target.value);cur=e.target.value;editing=null;render();}};
  $("clearDone").onclick=function(){tasks().forEach(function(t){if(t.done&&!t.hidden)t.hidden="cleared";});save();render();};
  $("carry").onclick=function(){
    var py=shift(cur,-1), src=state.tasks[py]||[];
    var moved=src.filter(function(t){return !t.done&&!t.hidden;});
    moved.forEach(function(t){tasks().push(t);});
    state.tasks[py]=src.filter(function(t){return moved.indexOf(t)<0;});
    save(py); save(cur); render();
  };
  document.querySelectorAll(".filters button").forEach(function(b){
    b.onclick=function(){
      filter=b.dataset.f;
      document.querySelectorAll(".filters button").forEach(function(x){x.setAttribute("aria-pressed",x===b?"true":"false");});
      render();
    };
  });

  function status(t){return t.hidden?"removed":(t.done?"done":"open");}
  function renderHist(){
    var rows=[];
    Object.keys(state.tasks).forEach(function(d){(state.tasks[d]||[]).forEach(function(t){rows.push({d:d,t:t});});});
    var c={Total:rows.length,Done:0,Open:0,Removed:0};
    rows.forEach(function(r){var s=status(r.t);c[s==="done"?"Done":s==="open"?"Open":"Removed"]++;});
    var st=$("stats"); st.textContent=""; st.className=fx.histIn?"stats sin":"stats";
    Object.keys(c).forEach(function(k){var e=document.createElement("div");e.className="stat";var b=document.createElement("b");b.textContent=c[k];var sp=document.createElement("span");sp.textContent=k;e.appendChild(b);e.appendChild(sp);st.appendChild(e);});
    var q=$("q").value.trim().toLowerCase(), f=$("hf").value;
    var shown=rows.filter(function(r){return (f==="all"||status(r.t)===f)&&(!q||r.t.text.toLowerCase().indexOf(q)>=0);});
    shown.sort(function(a,b){return a.d<b.d?1:a.d>b.d?-1:0;});
    var box=$("histList"); box.textContent=""; box.className=fx.histIn?"hin":""; fx.histIn=false;
    if(!shown.length){var em=document.createElement("div");em.className="empty";em.textContent=rows.length?"No tasks match.":"No tasks recorded yet.";box.appendChild(em);return;}
    var last="";
    shown.forEach(function(r,i){
      if(r.d!==last){
        last=r.d;
        var h=document.createElement("button");h.className="dh";h.title="Open this day";
        h.textContent=parse(r.d).toLocaleDateString(undefined,{weekday:"long",month:"long",day:"numeric",year:"numeric"});
        h.onclick=function(){cur=r.d;setView(false);};
        box.appendChild(h);
      }
      var t=r.t,s=status(t),row=document.createElement("div");row.className="hrow p-"+t.pri; row.style.animationDelay=Math.min(i*30,420)+"ms";
      var tx=document.createElement("span");tx.className="htxt"+(s==="done"?" hdone":"");tx.textContent=t.text;row.appendChild(tx);
      var bd=document.createElement("span");bd.className="badge "+s;
      bd.textContent=s==="removed"?(t.hidden==="cleared"?"Cleared":"Deleted"):(s==="done"?"Done":"Open");row.appendChild(bd);
      if(t.hidden){var rs=document.createElement("button");rs.className="mini";rs.textContent="Restore";
        rs.onclick=function(){delete t.hidden;save(r.d);render();};row.appendChild(rs);}
      var fd=document.createElement("button");fd.className="mini";fd.textContent="Delete forever";
      fd.onclick=function(){
        if(fd.dataset.arm){state.tasks[r.d]=state.tasks[r.d].filter(function(x){return x!==t;});save(r.d);render();}
        else{fd.dataset.arm="1";fd.textContent="Click again to confirm";setTimeout(function(){fd.dataset.arm="";fd.textContent="Delete forever";},3000);}
      };
      row.appendChild(fd);
      box.appendChild(row);
    });
  }
  function setView(h){
    document.body.classList.toggle("hist",h);
    $("tabDay").setAttribute("aria-pressed",h?"false":"true");
    $("tabHist").setAttribute("aria-pressed",h?"true":"false");
    editing=null; fx.histIn=h; render();
  }
  $("tabDay").onclick=function(){setView(false);};
  $("tabHist").onclick=function(){setView(true);};
  $("q").oninput=renderHist; $("hf").onchange=renderHist;
  render();

})();
