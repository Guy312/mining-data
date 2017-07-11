<%@ taglib prefix="c" uri="http://www.springframework.org/tags" %>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>${title}</title>
    <link rel="stylesheet" href='<c:url value="${pageContext.request.contextPath}/resources/css/layout.css"/>'>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/jstree/3.2.1/themes/default/style.min.css"/>


    <script src="https://ajax.aspnetcdn.com/ajax/jQuery/jquery-3.1.1.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/jstree/3.2.1/jstree.min.js"></script>
    <script src="https://openlayers.org/en/v3.19.1/build/ol.js"></script>

    <script type="text/javascript"
            src='<c:url value="${pageContext.request.contextPath}/resources/scripts/script2.js"/>'></script>
    <script>var demo_num = ${demoNumber}</script>

    <!-- Latest compiled and minified CSS -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"
          integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous">

    <!-- Optional theme -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css"
          integrity="sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp" crossorigin="anonymous">

    <!-- Latest compiled and minified JavaScript -->
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"
            integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa"
            crossorigin="anonymous"></script>

</head>
<body onload="init()">
<div class="row">
    <div id="left_menu" class="col-md-3">
        <div id="jstree_demo_div"></div>
        <div class="button_block"><input id="test004" type="button" value="Deploy drone"/></div>
        <div class="button_block"><input id="pushToIM" type="button" value="Push place tree to IM"/></div>
        <div class="button_block"><input id="test005" type="button" value="Reset"/></div>
        <div id="data_block_3" class="data_block">
            <div id="data_3" class="data">
                <div id="text_3" class="text">
                    <h2>Drone 312CFK</h2>
                    <p>
                        <span class="row-header">Latitude:</span>
                        <span id="lat">-</span>
                    </p>
                    <p>
                        <span class="row-header">Longitude:</span>
                        <span id="lon">-</span>
                    </p>
                    <p>
                        <span class="row-header">Height:</span>
                        <span id="height">-</span>
                    </p>
                </div>
            </div>
        </div>
        <div class="section">
            <pre id="props"></pre>
        </div>
    </div>
    <div id="mapDiv"  class="col-md-9"></div>
</body>
</div>
</html>