var div4o=document.getElementsByClassName('fav fr');
var div6o=document.getElementsByClassName('playbg');

let btn_op = document.createElement('button');
btn_op.innerHTML='跳op';
if ('%s'=='true'){
    btn_op.style='float:left';
    div6o[0].insertBefore(btn_op,div4o[0]);
}
else{
    btn_op.style='float:right; margin-right:10px;';
    div6o[0].appendChild(btn_op);
};

function getVideo(){
    var v1=document.getElementsByTagName('iframe');
    var v2=(v1[0].contentWindow || v1[0].contentDocument); if (v2.document){v2=v2.document};
    return v2.getElementsByTagName('video')[0];
};

btn_op.onclick=function(){
    getVideo().currentTime += Math.round('%s');
    Settings.print('skip op');
};


$(function(){
    function listen(){
        var time_log=0;
        let time_show_div = document.createElement('p');
        time_show_div.style='float:left; margin-left:15px; margin-top:5px; font-size:14px; color:gray;';
        time_show_div.innerText='0:00';
        div6o[0].insertBefore(time_show_div,div4o[0]);

        if ('%s'=='true'){
            Settings.print('can skip');
            getVideo().currentTime='%s';
        };

        getVideo().addEventListener('timeupdate',function(){
            var time_current=Math.ceil(this.currentTime)-1;
            if (time_current-time_log>=1 || time_current-time_log<-1){
                time_log=parseInt(time_current);
                var tail=time_log %% 60;
                if (tail<10){tail='0'+tail;};
                if (time_log>=3600){
                    var time_show=parseInt(time_log/3600)+':'+parseInt((time_log %% 3600)/60)+':'+tail;
                } else{var time_show=parseInt(time_log/60)+':'+tail;};
                time_show_div.innerText=time_show;

                var msg='{"'+'%s' +'":'+ '"'+'%s'+','+time_log+'"}';
                Settings.show_time_play(msg);
            };
        })
    };

    if ('%s'=='true'){
        var times_for_load = 1000; Settings.print('allow to log');
        var interval = setInterval(function(){
            if (getVideo() || times_for_load <0){
                clearInterval(interval);
                listen();
            } else{
                times_for_load = times_for_load -1;
                Settings.print('尝试次数：'+ ( 1000 - times_for_load ) );
            }
        }, 100);
    };

    Settings.print('load successfully');
})