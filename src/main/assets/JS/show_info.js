if (document.getElementById('ad_setting')) {
    var div_ad_p1=document.getElementById('ad_p1');
    let div_ad_number=document.createElement('p');
    div_ad_number.innerHTML='：%d';
    div_ad_p1.appendChild(div_ad_number);

    var div_ad_p2_summary=document.getElementById('ad_p2');
    let div_ad_details=document.createElement('div');
    div_ad_details.innerHTML='<p>%s</p>';
    div_ad_details.style='font-size:12px';
    div_ad_p2_summary.appendChild(div_ad_details);

    var playLog_number_div=document.getElementById('playLog_number');
    let playLog_number=document.createElement('div');
    playLog_number.innerHTML='<p>：%d</p>';
    playLog_number.style='float:left';
    playLog_number_div.appendChild(playLog_number);

    var clear_logs=document.getElementById('clear_logs');
    clear_logs.onclick=function(){
        if (confirm('确认清空记录？')){
            Settings.clear_logs();
            playLog_number.innerHTML='<p>：0</p>';
        }
    }
}