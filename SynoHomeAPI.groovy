/*
 * Synology Home Mode Switch
 *
 * Calls the Web API for Synology Surviellence Station to enable or disable Home Mode
 * 
 */
metadata {
    definition(name: "Synology Web API - Home Mode", namespace: "community", author: "Tom Quinn") {
        capability "Actuator"
        capability "Switch"
        capability "Sensor"
    }
}

preferences {
    section("URIs") {
        input "synoIP", "text", title: "Synology IP address or Hostname", required: true
        input "synoPort", "text", title: "Synology DSM Port number", required: true
        input "userName", "text", title: "Username", required: true
        input "passWord", "text", title: "Password", required: true
        input name: "noSSLCheck", type: "bool", title: "Disable SSL Checks", defaultValue: false
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def sSLCheckOff(){
    log.warn "SSL Checks disabled..."
    device.updateSetting("noSSLCheck",[value:"true",type:"bool"])
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "SSL Checking Disabled: ${noSSLCheck == true}"
    if (logEnable) runIn(1800, logsOff)
    if (noSSLCheck) runIn(1900,sSLCheckOff)
}

def parse(String description) {
    if (logEnable) log.debug(description)
}

def authSyno() {
    def authURL = [
        uri: "https://${synoIP}:${synoPort}/webapi/auth.cgi?api=SYNO.API.Auth&method=Login&version=3&account=${userName}&passwd=${passWord}&session=SurveillanceStation&format=sid",
        contentType: 'application/json',
        textParser: 'true',
        ignoreSSLIssues: "${noSSLCheck == true}" 
    ]
    
    if (logEnable) log.debug "Sending Auth GET request to [${authURL}]"

    try {
        httpGet(authURL) { resp ->
            if (logEnable)
                if (resp.data) log.debug "Raw data: ${resp.data}"
        log.debug "SID: ${resp.data.data.sid}"
        return resp.data.data.sid
        }
    } catch (Exception e) {
        log.warn "Call to Auth failed: ${e.message}"
    }
}
def deAuthSyno() {
        def deauthURL = [
        uri: "https://${synoIP}:${synoPort}/webapi/auth.cgi?api=SYNO.API.Auth&method=Logout&version=3&account=${userName}&passwd=${passWord}&session=SurveillanceStation",
        contentType: 'application/json',
        textParser: 'true',
        ignoreSSLIssues: "${noSSLCheck == true}"
    ]
     if (logEnable) log.debug "Sending DeAuth GET request to [${deauthURL}]"

    try {
        httpGet(deauthURL) { resp ->
            if (logEnable) if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to DeAuth failed: ${e.message}"
    }
}

def on() {
    def urlToken = authSyno()
    if (logEnable) log.debug "urlToken: ${urlToken}"

    def onURL = [
        uri: "https://${synoIP}:${synoPort}/webapi/entry.cgi?api=SYNO.SurveillanceStation.HomeMode&version=1&method=Switch&on=true&_sid=${urlToken}",
        contentType: 'application/json',
        textParser: 'true',
        ignoreSSLIssues: "${noSSLCheck == true}"
    ]
    try {
        httpGet(onURL) { resp ->
            if (resp.success) {
                sendEvent(name: "switch", value: "on", isStateChange: true)
            }
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
    
    deAuthSyno()
}

def off() {
    def urlToken = authSyno()
    if (logEnable) log.debug "urlToken: ${urlToken}"
    def offURL = [
        uri: "https://${synoIP}:${synoPort}/webapi/entry.cgi?api=SYNO.SurveillanceStation.HomeMode&version=1&method=Switch&on=false&_sid=${urlToken}",
        contentType: 'application/json',
        textParser: 'true',
        ignoreSSLIssues: "${noSSLCheck == true}"
    ]
     if (logEnable) log.debug "Sending on GET request to [${offURL}]"

    try {
        httpGet(offURL) { resp ->
            if (resp.success) {
                sendEvent(name: "switch", value: "off", isStateChange: true)
            }
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to off failed: ${e.message}"
    }
    
}
