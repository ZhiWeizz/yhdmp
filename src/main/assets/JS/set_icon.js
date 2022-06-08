var div_head=document.getElementsByClassName('nav fr')[0];
div_head.removeChild(document.getElementById('nav_fr_to_pc'));
let btn_setting = document.createElement('button');

btn_setting.style='position:absolute;right:88px;background-color:transparent;color:#FFFFFF;border:2.4px solid white;border-radius:50%;display:right;height:23px;width:23px;margin-top:10px;text-align:center;font-family:Consolas;font-weight:bold;';
btn_setting.textContent='S';
btn_setting.title='设置';
div_head.insertBefore(btn_setting,div_head.childNodes[0]);

btn_setting.onclick=function(){
    btn_setting.style.border='2.4px solid #66ccff';
    Settings.load_settings();
}