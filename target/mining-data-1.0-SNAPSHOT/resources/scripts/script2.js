/**
 * Created by 502669124 on 4/25/2017.
 */
/**
 * Generates a GUID string.
 * @returns {String} The generated GUID.
 * @example af8a8416-6e18-a307-bd9c-f2c947bbb3aa
 * @author Slavik Meltser (slavik@meltser.info).
 * @link http://slavik.meltser.info/?p=142
 */
function guid() {
    function _p8(s) {
        var p = (Math.random().toString(16) + "000000000").substr(2, 8);
        return s ? "-" + p.substr(0, 4) + "-" + p.substr(4, 4) : p;
    }

    return _p8() + _p8(true) + _p8(true) + _p8();
}

class MovingThings {
    constructor(vs) {
        this.mos = [];
        this.myVar = setInterval(this.mytick.bind(this), 100);
        this.vs = vs;
    }

    mytick() {
        for (var i = 0; i < this.mos.length; i++) {
            if (this.mos[i].OnMoving) {
                this.mos[i].nextPosition(this.vs);
            }
        }
    }

    clear() {
        for (var i = 0; i < this.mos.length; i++) {
            this.mos[i].remove(this.vs);
        }
        this.mos = [];
    }

}

class MyAlert {
    constructor(msg, id) {
        this.id = id || "#alert";
        this.msg = msg;
    }

    show() {
        $(this.id).html(this.msg);
        $(this.id).css("display", "block");
    }

    hide() {
        $(this.id).html("");
        $(this.id).css("display", "none");
    }
}

class MovingObject {
    constructor(lat, lon, h) {
        if ((lat == undefined) || (lon == undefined)) {
            this.pos = undefined;
        } else {
            if (h == undefined) {
                h = 0;
            }
            this.pos = [lat, lon, h];
        }
        this.path = [];
        this.OnPointRequest = false;
        this.OnMoving = true;
        this.id = guid();
        this.percent = 0;
        this.color = '#000000';
        this.pathName = "none";
        this.d = 0.00005;
        this.perFunc = function (x) {
            if (x > 100) {
                return x - 100;
            } else {
                return x;
            }
        }
    }

    addPathPoint(lat, lon, h) {
        this.path.push([lat, lon, h]);
        //  console.log("ADD POINT TO PATH");
        //  console.log(this.path);
    }

    requestNextPoint() {
        this.percent = this.perFunc(this.percent + 1);

        $.get("/drone/path/" + this.pathName + "/" + this.percent, function (data, status) {
            //console.log("PATH POINT");
            //console.log(data);
            var pnt = JSON.parse(data)
            this.addPathPoint(pnt[0], pnt[1], pnt[2] || 2);
            this.OnPointRequest = false;
        }.bind(this));

    }

    nextPosition(vectorSource) {
        if (this.path.length < 3 && (!this.OnPointRequest)) {
            this.OnPointRequest = true;
            this.requestNextPoint();

        } else if (this.path.length >= 2) {


            console.log("this.pos:");
            console.log(this.pos);
            if (this.pos == undefined) {
                console.log("in");
                this.pos = this.path[0];
            }
            //console.log("out");

            var x0 = this.pos[0];
            var y0 = this.pos[1];
            var z0 = this.pos[2] || 0;
            var x1 = this.path[1][0];
            var y1 = this.path[1][1];
            var z1 = this.path[1][2] || 0;
            var l = Math.sqrt(Math.pow((x0 - x1), 2) + Math.pow((y0 - y1), 2));
            if (l < this.d) {
                this.path.shift();
                this.pos = this.path[0];
            } else {
                var x = x0 + (x1 - x0) * this.d / l;
                var y = y0 + (y1 - y0) * this.d / l;
                var z = z0 + (z1 - z0) * this.d / l;
                this.pos = [x, y, z];
            }
            this.updatePosition(vectorSource);
        }
    }

    updatePosition(vectorSource) {
        //console.log("UPDATE POSITION");
        //console.log(this.pos);
        locationDataBlock.Lat = this.pos[0];
        locationDataBlock.Lon = this.pos[1];
        locationDataBlock.h = this.pos[2];
        locationDataBlock.update();

        if ((this.pos[1] > 41.09595) || (this.pos[1] < 41.08745)) {
            (new MyAlert("DRONE OUTSIDE FACILITY BOUNDARY !!")).show();
            dataBlock.time_photo_on = false;
            dataBlock.time_flight_on = false;

            this.color = '#ff0000';
            this.OnMoving = false;
        }
        this.remove(vectorSource);
        this.show(vectorSource);
    }

    show(vectorSource) {
        var feature1 = new ol.Feature({
            'geometry': new ol.geom.Point(ol.proj.fromLonLat(this.pos))
        });
        feature1.setStyle(new ol.style.Style({
            image: new ol.style.Circle({
                radius: 4,
                fill: new ol.style.Fill({color: this.color}),
                stroke: new ol.style.Stroke({color: '#000000', width: 1})
            })
        }));
        feature1.setId(this.id);
        vectorSource.addFeature(feature1);
    }

    remove(vectorSource) {
        var feature = vectorSource.getFeatureById(this.id);

        // console.log(feature);
        if (feature != null) {
            vectorSource.removeFeature(feature);
        }
    }

}

class DataBlock {
    constructor(data) {
        this.time_flight = data.time_flight || 0;
        this.time_photo = data.time_photo || 0;
        this.time_flight_on = data.time_flight_on || false;
        this.time_photo_on = data.time_photo_on || false;
        this.myVar = null;
    }

    turnOff() {
        $('#data_block').css("display", "none");
        clearInterval(this.myVar);
    }

    turnOn() {
        this.update();
        $('#data_block').css("display", "block");
        this.myVar = setInterval(this.mytick.bind(this), 1000);
    }

    mytick() {
        if (this.time_flight_on) {
            this.time_flight++;
        }
        if (this.time_photo_on) {
            this.time_photo++;
        }
        this.update();
    }

    formatTime(sec) {
        var minutes = Math.floor(sec / 60);
        var seconds = sec - (minutes * 60);
        if (minutes < 10) {
            minutes = "0" + minutes;
        }
        if (seconds < 10) {
            seconds = "0" + seconds;
        }
        return minutes + ':' + seconds;
    }

    update() {
        [['#time_flight', this.time_flight],
            ['#time_photo', this.time_photo]].forEach(function (time) {
            if (undefined != time[1]) {
                $(time[0]).html(this.formatTime(time[1]));
            } else {
                $(time[0]).html('0:00');
            }
        }.bind(this))

    }
}

class CountDataBlock {
    constructor(data) {
        this.forkliftCount = data.forkliftCount || 0;
        this.trackCount = data.trackCount || 0;
    }

    turnOff() {
        $('#data_block_2').css("display", "none");
    }

    turnOn() {
        $('#data_block_2').css("display", "block");
    }

    addForklift() {
        this.forkliftCount++;
        this.update();
    }

    update() {
        [['#forklifts', this.forkliftCount],
            ['#tracks', this.trackCount]].forEach(function (cnt) {
            if (undefined != cnt[1]) {
                $(cnt[0]).html(cnt[1]);
            } else {
                $(cnt[0]).html('0');
            }
        }.bind(this))

    }
}

class LocationDataBlock {
    constructor(data) {
        this.Lat = data['Lat'];
        this.Lon = data['Lon'];
        this.h = data['h'];
    }

    turnOff() {
        $('#data_block_3').css("display", "none");
    }

    turnOn() {
        $('#data_block_3').css("display", "block");
    }

    toFixed(num, fixed) {
        var re = new RegExp('^-?\\d+(?:\.\\d{0,' + (fixed || -1) + '})?');
        return num.toString().match(re)[0];
    }

    heightFormat(h) {
        if (h == undefined) return undefined;
        return this.toFixed(h, 2) + "m";
    }

    dms(a0) {
        var a = Math.abs(a0);
        var d = Math.trunc(a);
        var m = Math.trunc((a - d) * 60);
        var s = this.toFixed((a - d - m / 60) * 60 * 60, 2);

        return Math.sign(a0) * d + '\u00B0' + m + "'" + s + "\"";
    }

    update() {
        [['#lat', this.dms(this.Lat)],
            ['#lon', this.dms(this.Lon)],
            ['#height', this.heightFormat(this.h)]].forEach(function (cnt) {
            if (undefined != cnt[1]) {
                $(cnt[0]).html(cnt[1]);
            } else {
                $(cnt[0]).html('-');
            }
        }.bind(this))

    }
}


var map;
var vectorSource;
var formatter;
var dataBlock = new DataBlock({time_flight: 0, time_photo: 0});
var mo = new MovingObject(-76.14639043807983, 41.095);
var mos;
var countDataBlock;
var locationDataBlock = new LocationDataBlock({});
var blackStyle = {
    fill: new ol.style.Fill({
        color: 'rgba(255, 100, 50, 0.0)'
    }),
    stroke: new ol.style.Stroke({
        width: 2,
        color: 'rgba(0, 0, 0, 1)'
    })
}

var redStyle = {
    fill: new ol.style.Fill({
        color: 'rgba(255, 50, 50, 0.5)'
    }),
    stroke: new ol.style.Stroke({
        width: 2,
        color: 'rgba(255, 50, 50, 1)'
    })
}


function init() {
    vectorSource = new ol.source.Vector();
    mos = new MovingThings(vectorSource);
    formatter = new ol.format.GeoJSON();

    var url = "https://services.arcgisonline.com/arcgis/rest/services/World_Imagery/MapServer";

    var mapLayers = [
        new ol.layer.Tile({
            source: new ol.source.OSM()
        }),
        new ol.layer.Image({
            source: new ol.source.ImageArcGISRest({
                ratio: 1,
                params: {},
                url: url
            })
        }),
        new ol.layer.Vector({
            source: vectorSource,
            style: null
        })
    ];

    map = new ol.Map({
        layers: mapLayers,
        target: 'mapDiv',
        view: new ol.View({
            center: ol.proj.transform([-76.1472675204277, 41.090747328856416], 'EPSG:4326', 'EPSG:3857'),
            zoom: 14,
            maxZoom: 21
        })
    });


    // deploy drone
    $("#test001").click(function () {
        dataBlock.time_flight_on = true;
        dataBlock.turnOn();
        if (demo_num == 2) {
            mo.pathName = "back"
        }
        mo.show(vectorSource);
        mos.mos.push(mo);
    });

    // reset
    $("#test002").click(function () {
        dataBlock.turnOff();
        dataBlock = new DataBlock({});
        mos.clear();
        mo = new MovingObject(-76.1455, 41.094);
        $("#alert").html("");
        $("#alert").css("display", "none");
    });

    // start photo
    $("#test003").click(function () {
        if (dataBlock.time_photo_on) {
            dataBlock.time_photo_on = false;
            $("#test003").val("Start photo");
        } else {
            dataBlock.time_photo_on = true;
            $("#test003").val("End photo");
        }

    });

    // deploy drone on path
    $("#test004").click(function () {
        var mo2 = new MovingObject();
        mo2.pathName = "dronepath_path001";
        mo2.d = 0.00002;

        mos.mos.push(mo2);
        console.log("deploy drone on path")
    });

    // reset drone on path
    $("#test005").click(function () {
        mos.clear();
        console.log("reset drone on path")
    });

    $("#sendForklift").click(function () {
        if (countDataBlock == undefined) {
            countDataBlock = new CountDataBlock({trackCount: 2});
            countDataBlock.turnOn();
        }
        countDataBlock.addForklift();
        if (countDataBlock.forkliftCount > 4) {
            (new MyAlert("Too many forklifts in section!")).show();
        }

        var mo2 = new MovingObject();
        mo2.pathName = "forklift_path001";
        mo2.d = 0.000005;
        mo2.perFunc = function (x) {
            if (x > 32) {
                return (x - 32) % 55 + 32;
            } else {
                return x;
            }
        };
        mos.mos.push(mo2);
    });


    function readFeature(Feature) {
        var features = formatter.readFeatures(Feature, {
            dataProjection: 'EPSG:4326',
            featureProjection: 'EPSG:3857'
        });
        return features[0]
    }

    function showFeature(Feature, stl) {
        Feature.setStyle(
            new ol.style.Style(stl || blackStyle)
        );
        vectorSource.addFeatures([Feature]);
    }

    function zoomToFeature(Feature) {
        map.getView().fit(Feature.getGeometry().getExtent(), map.getSize());
    }


    function addFeatureToMapAndZoom(Feature) {
        var features = readFeature(Feature)
        showFeature(features)
        zoomToFeature(features);
    }


    // load the place tree
    $.get("/osm_place_tree", function (data, status) {

        $('#jstree_demo_div').jstree({
            'core': {
                'data': JSON.parse(data)
            }
        }).bind("loaded.jstree", function () {
            var selectedPlace = "London Borough of Hillingdon";

            var sus = $.grep(JSON.parse(data), function (e) {
                return e.text == selectedPlace;
            });
            $('#jstree_demo_div').jstree("select_node", sus[0].id.toString()).trigger("select_node.jstree");
        });


        $('#jstree_demo_div').on("changed.jstree", function (e, sdata) {
            id = sdata.selected[0];
            var result = $.grep(JSON.parse(data), function (e) {
                return e.id == id;
            });

            currentPlaceName = result[0].text;
            $.get("/polygon/".concat(result[0].text), function (data, status) {
                var dataJson = JSON.parse(data);
                $('#props').html(JSON.stringify(dataJson.properties, null, 2));
                if (dataJson.geometry == undefined) {
                    var featuredata = dataJson.geometries[0];
                } else {
                    var featuredata = dataJson.geometry;
                }
                addFeatureToMapAndZoom(featuredata);
            });
        });


        var zoomTo = 'London Borough of Hillingdon';
        var placesToShow = ['London Borough of Hillingdon', "Heathpark Golf Course",
            "Heathpark Golf Course",
            "Stockley Road Lake",
            "Maysfields Lake",
            "Cherry Lane Cemetery",
            "Station Road Allotments",
            "Pinkwell Park",
            "British Airways Waterside",
            "Hilton London Heathrow Airport",
            "Easyhotel",
            "Harlington Locomotive Society",
            "Holiday Inn",
            "Imperial College Harlington Sports Ground",
            "Marriott Hotel",
            "Sheraton Skyline",
            "Heathrow Terminal 5",
            "Victoria Lane Burial Ground",
            "Little Harlington Playing Fields",
            "Holiday Inn",
            "Park Inn Heathrow",
            "Leonardo Hotel",
            "Sipson Recreation Ground",
            "Sipson Hall",
            "Heathrow Terminal 5B",
            "Heathrow Terminal 5C",
            "Heathrow Terminal 5 short stay car park",
            "Hertz",
            "Sovereign Court",
            "BAA Heathrow",
            "Moor Lane Allotment Site",
            "Airplot",
            "Rackspace Car park",
            "Rackspace",
            "Former Bmi Hangar",
            "TCR Combined Motor Transport Facility",
            "Catering Centre",
            "E2 Staff Car Park",
            "N 4 Staff Parking",
            "Compass Centre",
            "Compass Centre Parking",
            "N2",
            "West Drayton Cemetery allotments",
            "West Drayton Depot allotments",
            "Prologis Park",
            "Avis Heathrow",
            "Staff Parking",
            "Goals",
            "Cranford Park Visitor Centre",
            "Bourne Farm Allotments",
            "Hayes Fire Station",
            "Purple Parking",
            "Thistle Hotel",
            "Business Parking Terminal 5",
            "Heathrow Ultra Depot",
            "N5",
            "Cargo Apron 455",
            "Cargo Apron 511",
            "Cargo Apron 551",
            "Virgin Hangar",
            "T5 coach park",
            "Arora_Sofitel",
            "Premier Inn",
            "The Closes Recreation Ground",
            "Enterprise rent-a-car",
            "EuropCar_National",
            "Heathrow Police Station",
            "Terminal 2A",
            "Harmondsworth Barn",
            "Saxon Lake",
            "Swan Lake",
            "Terminal 4",
            "Cargo Terminal",
            "T3IB",
            "Heathrow Terminal 3",
            "Heathrow Terminal 1A Short Stay",
            "LHR Terminal 2 Business Parking",
            "Heathrow Terminal 1 Short Stay",
            "Royal Suite",
            "Terminal 3",
            "N1",
            "North Escape Shaft",
            "South Escape Shaft",
            "Wessex Road Shaft",
            "Sanctuary Road Escape Shaft",
            "PiccEx drivers escape shaft",
            "Heathrow Ultra Operation Centre",
            "Car Park 3 Shaft",
            "Hatton Cross Tube station",
            "Heathrow Terminal 1",
            "Heathrow Central Bus station 923",
            "Heathrow Central Bus station 968",
            "Heathrow Central Bus station 059",
            "North Ventilation Shaft",
            "South Ventilation Shaft",
            "Vent Shaft 251",
            "Vent Shaft 289",
            "Vent Shaft 327",
            "Vent Shaft 365",
            "Vent Shaft 403",
            "Vent Shaft 441",
            "Vent Shaft 479",
            "Panalpina World Transport",
            "Premier Inn Heathrow Terminal 5",
            "McDonald's",
            "Hilton Garden Inn London Heathrow Airport",
            "Heathrow Airport Coach Park",
            "Heathrow Police Compound",
            "Customs House 073",
            "Renaissance London Heathrow Hotel 115",
            "Customs House 181",
            "Renaissance London Heathrow Hotel 219",
            "HMRC 321",
            "British Telecom Computer Centre",
            "HMRC 135",
            "Academy Visitors",
            "Heathrow Academy",
            "British Airways West Base",
            "West Base Carpark",
            "British Airways Technical Block",
            "Heathrow Animal Reception Centre",
            "Control Tower",
            "Infinity Stockley Park",
            "DB Schenker",
            "Texico",
            "St. Dunstan's",
            "Wildlife Garden",
            "The Three Magpies 244",
            "The Three Magpies 289",
            "St. George's Interdenominational Chapel",
            "Airport Fire Station",
            "Northern Fuel Farm",
            "Arrivals",
            "Departures",
            "Terminal 1 Departure Lounge",
            "Parkwest Appartments",
            "Drayton Garden Village",
            "Mulberry Park",
            "West Drayton Library",
            "Heathrow Coach Centre",
            "Aircraft Viewing Area",
            "Asda Petrol",
            "Mercure Hotel Heathrow",
            "Southern Fuel Farm",
            "Rolls Royce 975",
            "Rolls Royce 027",
            "Voyager 583",
            "Voyager 620",
            "Heathrow Fire and Ambulance Station",
            "Langthorne Park",
            "TBC"
        ];


        placesToShow.forEach(function (element) {
            $.get("/polygon/".concat(element), function (data, status) {
                var dataJson = JSON.parse(data);
                // $('#props').html(JSON.stringify(dataJson.properties, null, 2));
                if (dataJson.geometry == undefined) {
                    var feature = readFeature(dataJson.geometries[0]);
                } else {
                    var feature = readFeature(dataJson.geometry);
                }

                showFeature(feature)
                if (zoomTo == element) {
                    zoomToFeature(feature);
                    console.log("zoomed")
                }
            });
        })

    });

}


var osm2geo = function (osm) {
    // Check wether the argument is a Jquery object and act accordingly
    // Assuming it as a raw server response for now
    var $xml = jQuery(osm);
    // Initialize the empty GeoJSON object
    var geo = {
        "type": "FeatureCollection",
        "features": []
    };
    // setting the bounding box [minX,minY,maxX,maxY]; x -> long, y -> lat
    function getBounds(bounds) {
        var bbox = new Array;
        bbox.push(parseFloat(bounds.attr("minlon")));
        bbox.push(parseFloat(bounds.attr("minlat")));
        bbox.push(parseFloat(bounds.attr("maxlon")));
        bbox.push(parseFloat(bounds.attr("maxlat")));
        return bbox;
    }

    geo["bbox"] = getBounds($xml.find("bounds"));

    // Function to set props for a feature
    function setProps(element) {
        var properties = {};
        var tags = $(element).find("tag");
        tags.each(function (index, tag) {
            properties[$(tag).attr("k")] = $(tag).attr("v");
        });
        return properties;
    }

    // Generic function to create a feature of given type
    function getFeature(element, type) {
        return {
            "geometry": {
                "type": type,
                "coordinates": []
            },
            "type": "Feature",
            "properties": setProps(element)
        };
    }

    // Ways
    var $ways = $("way", $xml);
    $ways.each(function (index, ele) {
        var feature = new Object;
        // List all the nodes
        var nodes = $(ele).find("nd");
        // If first and last nd are same, then its a polygon
        if ($(nodes).last().attr("ref") === $(nodes).first().attr("ref")) {
            feature = getFeature(ele, "Polygon");
            feature.geometry.coordinates.push([]);
        } else {
            feature = getFeature(ele, "LineString");
        }
        nodes.each(function (index, nd) {
            var node = $xml.find("node[id='" + $(nd).attr("ref") + "']"); // find the node with id ref'ed in way
            var cords = [parseFloat(node.attr("lon")), parseFloat(node.attr("lat"))]; // get the lat,lon of the node
            // If polygon push it inside the cords[[]]
            if (feature.geometry.type === "Polygon") {
                feature.geometry.coordinates[0].push(cords);
            }// if just Line push inside cords[]
            else {
                feature.geometry.coordinates.push(cords);
            }
        });
        // Save the LineString in the Main object
        geo.features.push(feature);
    });

    // Points (POI)
    var $points = $("node:has('tag')", $xml);
    $points.each(function (index, ele) {
        var feature = getFeature(ele, "Point");
        feature.geometry.coordinates.push(parseFloat($(ele).attr('lon')));
        feature.geometry.coordinates.push(parseFloat($(ele).attr('lat')));
        // Save the point in Main object
        geo.features.push(feature);
    });
    // Finally return the GeoJSON object
    return geo;

};