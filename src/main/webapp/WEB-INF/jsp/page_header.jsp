<%@ taglib prefix='security' uri='http://www.springframework.org/security/tags' %>
   <div id="header-container">
      <div id="logo">
         <h1>
            <a href="#" onclick="window.open('about.html','AboutWin','toolbar=no, menubar=no,location=no,resizable=no,scrollbars=yes,statusbar=no,top=100,left=200,height=650,width=450');return false"><img alt="" src="/img/img-auscope-banner.gif"></a>
            <!-- <a href="login.html"><img alt="" src="/img/img-auscope-banner.gif" /></a> -->
         </h1>
      </div>
                                  
      <security:authorize ifAllGranted="ROLE_ADMINISTRATOR">
         <a href="admin.html"><span>Administration</span></a>
      </security:authorize>
      
      <STYLE type="text/css">
        H2 { text-align: center}
        #nav-example-02 a {
            background: url("../img/navigation.gif") -100px -38px no-repeat;
        }
      </STYLE>
      
      <div id="menu">
         <ul >
            <li <%if (request.getRequestURL().toString().contains("/gmap.jsp")) {%>class="current" <%} %>><a href="gmap.html">Map Client<span></span></a></li>
            <security:authorize ifAllGranted="ROLE_JOB_SUBMISSION"> </security:authorize>
            
            <li <%if (request.getRequestURL().toString().contains("/gridsubmit.jsp")) {%>class="current" <%} %>><a href="gridsubmit.html">Submit Jobs<span></span></a></li>
            <li <%if (request.getRequestURL().toString().contains("/joblist.jsp")) {%>class="current" <%} %>><a href="joblist.html">Monitor Jobs<span></span></a></li>
            
            
            <security:authorize ifAllGranted="ROLE_ANONYMOUS">
            	<li ><a href="login.html">Login<span></span></a></li>
            </security:authorize>
            
            <security:authorize ifNotGranted="ROLE_ANONYMOUS">
            	<li ><a href="j_spring_security_logout">Logout<span></span></a></li>
            </security:authorize>
         </ul>
      </div>
   </div>
