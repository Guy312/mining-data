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
    <div class="button_block"><input id="sendForklift" type="button" value="Send forklift"/></div>
    <div class="button_block"><input id="returnForklift" type="button" value="Return forklift"/></div>
    <div id="alert_block"><div id="alert"></div></div>
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

    <div id="data_block_2"  class="data_block">
        <div id="data_2"  class="data">
            <div id="text_2" class="text">
                <h2>Sector 3</h2>
                <p>
                    <span class="row-header">Forklifts:</span>
                    <span id="forklifts">0</span>
                </p>
                <p>
                    <span class="row-header">Tracks:</span>
                    <span id="tracks">3</span>
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