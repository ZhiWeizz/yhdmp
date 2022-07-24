if (document.getElementById('author_setting')){
    var div2y=document.getElementById('skipop_left');
    var div2n=document.getElementById('skipop_right');
    if ('%s'==='true'){div2y.checked=true}
    else {div2n.checked=true};

    var div3s=document.getElementById('timeSet_skipOp');
    div3s.value='%d';

    var div4y=document.getElementById('history_check_yes');
    var div4n=document.getElementById('history_check_no');
    if ('%s'==='true'){div4y.checked=true}
    else {div4n.checked=true};

    var div5s=document.getElementById('play_history_during');
    div5s.value='%d';

    var div6y=document.getElementById('fix_screen_yes');
    var div6n=document.getElementById('fix_screen_no');
    if ('%s'==='true'){div6y.checked=true}
    else {div6n.checked=true};


    var settings_temp='nth';
    function save_settings(){
        var p1='has_been_aborted';
        var p2=div2y.checked;
        var p3=div3s.value;
        var p4=div4y.checked;
        var p5=div5s.value;
        var p6=div6y.checked;

        var settings_new= '{p1:'+p1+ ',p2:'+p2+ ',p3:'+p3+ ',p4:'+p4+ ',p5:'+p5+ ',p6:'+p6+ '}';
        if (settings_temp != settings_new){
            Settings.save_settings(settings_new)
        };
        settings_temp = settings_new;
    }
    setInterval(save_settings, 200);
}