/**
 *  VeSync Device Handler - Etekcity Plug
 *
 *  Copyright 2020 Chris Weber
 *
 *  Inspired by:
 *    https://github.com/hongtat/tasmota-connect
 *    Copyright 2020 AwfullySmart.com - HongTat Tan
 *    (GPL 3.0 License)
 *
 *    https://github.com/nicolaskline/smartthings
 *    (Specifically etekcity-plug.src)
 *    Author/Copyright: nkline
 *    (Unknown License)
 *
 *    https://github.com/projectskydroid/etekcity_inwallswitch_api
 *    (Apache License)
 *
 *    https://github.com/webdjoe/pyvesync_v2
 *    (MIT License)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
String driverVersion() { return "20201009" }

metadata {
    definition (name: "Etekcity Plug", namespace: "oschrich", author: "Chris Weber") {
        capability "Energy Meter"
        capability "Actuator"
        capability "Switch"
        capability "Power Meter"
        capability "Voltage Measurement"
        capability "Refresh"
        capability "Configuration"
        capability "Sensor"
        capability "Light"
        //capability "Health Check"
        capability "Polling"

        command "on"
        command "off"
        attribute "device_id", "string"
        attribute "device_type", "string"
    }

    // simulator metadata
    simulator {
    }

    // UI tile definitions
    tiles(scale: 2) {
        multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4, canChangeIcon: true){
            tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
                attributeState("on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC", nextState:"turningOff")
                attributeState("off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState:"turningOn")
                attributeState("turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff")
                attributeState("turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn")
            }
        }
        valueTile("power", "device.power", width: 2, height: 2) {
            state "default", label:'${currentValue} W'
        }
        valueTile("energy", "device.energy", width: 2, height: 2) {
            state "default", label:'${currentValue} kWh'
        }
        standardTile("reset", "device.reset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:'reset kWh', action:"reset"
        }
        standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:'', action:"refresh", icon:"st.secondary.refresh"
        }
        main(["switch","power","energy"])
        details(["switch","power","energy","refresh","reset"])
    }
}


def updated() {
    log.trace("Updated")
    
    initialize()
}

def installed() {
    log.trace("installed")
    
    initialize()
}

def doActionOld(String action) {
    log.trace "Action (old): ${action}"
    
    webPut("/v1/" + device.currentValue("device_type") + "/" + device.currentValue("device_id") + "/status/" + action)
}

def doAction15A(String action) {
    log.trace "Action (15A): ${action}"
    
    def uuid = "28ae62dd-593b-42c3-b59a-4003b979bdf1"

    def body = '{"timeZone": "America/New_York","acceptLanguage": "en","accountID": "' + parent.state.accountId + '","token": "' + parent.state.token + '","uuid": "' + uuid + '","status": "' + action + '"}'

    jsonPut("/15a/v1/device/devicestatus")
}

private initialize() {
    log.trace "initialize"
    
    if (!device.currentValue("device_id")) {
        setDeviceId()
    }

    if (!device.currentValue("device_type")) {
        def type = parent.getDeviceType(device.getDeviceNetworkId())
    
        sendEvent(name: "device_type", value: type)
    }
    
    log.trace "Device ID: ${device.currentValue("device_id")}"
    log.trace "Device Type: ${device.currentValue("device_type")}"
}

void setDeviceId() {
    def dni = device.getDeviceNetworkId()
    log.trace "Getting device ID for ${dni}"

    def cid = parent.getDeviceCID(dni)
    
    sendEvent(name: "device_id", value: cid)
}

private webGet(path) {

    return parent.webGet(path)
}

private webPut(path) {

    return parent.webPut(path)
}

private jsonPut(path, bodyMap) {

    return parent.jsonPut(path, bodyMap)
}


def refresh() {
    log.trace "refresh"

    initialize()

    def data = webGet("/v1/device/" + device.currentValue("device_id") + "/detail")
    log.trace data
    sendEvent(name: "switch", value: data["deviceStatus"]) 
    sendEvent(name: "energy", value: data["energy"])
    if(device.currentValue("device_type") == "ESW15-USA" ) {
    	log.trace "Made it here"
        sendEvent(name: "power", value: data["power"])
        sendEvent(name: "voltage", value: data["voltage"])
    }
    else {
        sendEvent(name: "power", value: parent.convertHex(data["power"])) 
        sendEvent(name: "voltage", value: parent.convertHex(data["voltage"])) 
    }
    String interval = "601"
    if (!device.currentValue("checkInterval") || device.currentValue("checkInterval") != interval) {
        sendEvent(name: "checkInterval", value: interval, displayed: true) 
    }
}

def poll() {
    log.trace "Poll"
    refresh()
}

def ping() {
    log.trace "Ping"
    refresh()
}

def on() {
    sendEvent(name: "switch", value: "on") 
    if(device.currentValue("device_type") == "ESW15-USA" ) {
        return doAction15A("on")
    }
    else {
        return doActionOld("on")
    }
}

def off() {
    sendEvent(name: "switch", value: "off") 
    if(device.currentValue("device_type") == "ESW15-USA" ) {
        return doAction15A("off")
    }
    else {
        return doActionOld("off")
    }
}

private delayAction(long time) {
    new physicalgraph.device.HubAction("delay $time")
}
