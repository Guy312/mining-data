<%@ taglib prefix="c" uri="http://www.springframework.org/tags" %>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>${title}</title>
    <link rel="stylesheet" href='<c:url value="${pageContext.request.contextPath}/resources/css/layout.css"/>'>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/jstree/3.2.1/themes/default/style.min.css" />

    <script src="https://ajax.aspnetcdn.com/ajax/jQuery/jquery-3.1.1.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/jstree/3.2.1/jstree.min.js"></script>
    <script src="https://openlayers.org/en/v3.19.1/build/ol.js"></script>
    <script type="text/javascript"
            src='<c:url value="${pageContext.request.contextPath}/resources/scripts/script.js"/>'></script>
    <script>var demo_num = ${demoNumber}</script>
</head>
<body onload="init()">
<div id="left_menu">
    <div id="jstree_demo_div"></div>
    <div class="button_block"><input id="test001" type="button" value="Deploy drone"/></div>
    <div class="button_block"><input id="test002" type="button" value="Reset"/></div>
    <div class="button_block"><input id="test003" type="button" value="Start photo"/></div>
    <div id="data_block" class="data_block">
        <div id="data" class="data">
            <div id="text" class="text">
                <h2>Drone 315FC</h2>
                <p>
                    <span class="row-header">Sector</span>
                    <span id="sector">4</span>
                </p>
                <p>
                    <span class="row-header">Flight time:</span>
                    <span id="time_flight">0:00</span>
                </p>
                <p>
                    <span class="row-header">Photo time:</span>
                    <span id="time_photo">0:00</span>
                </p>
            </div>
        </div>
    </div>
    <div class="section">
        <pre id="props"></pre>
    </div>
</div>
<div id="mapDiv" ></div>
</body>
</html>