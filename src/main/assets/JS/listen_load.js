document.onreadystatechange = function () {
    switch (document.readyState) {
        case 'interactive':
            var url_now = window.location.href;
            Settings.print('DOM已加载完成, 当前url为: '+url_now);
            Settings.load_js_dom(url_now);
            Settings.load_js_img('1000');
            Settings.load_js_img('3000');
            break;
        case 'complete':
            Settings.print('页面已完成加载, 包括css, 图片等。url为：'+window.location.href);
            Settings.load_js_img('0');
            break;
    };
}