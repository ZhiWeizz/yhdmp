if (document.getElementById('author_setting') === null){
    var div1 = document.getElementsByTagName('style');
    div1[div1.length - 1].nextElementSibling.click();
    var returnValue = div1[div1.length - 1].innerHTML;
    (function myFunction(value) {return value;})(returnValue);
}