$(function(){
      var headerScroll = 300;
      $(window).scroll(function() {
      var scroll = getScroll();
      if ( scroll >= headerScroll ) {
        $('.navbar-lp').addClass('scroll');
        $('.navbar-lp').addClass('scroll-bg');
        }
      else {
        $('.navbar-lp').removeClass('scroll');
        $('.navbar-lp').removeClass('scroll-bg');
        }
});


function getScroll() {
      return window.pageYOffset || document.documentElement.scrollTop;
      }
 });