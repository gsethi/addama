#!/usr/bin/env python

import socket
import httplib
import urllib
import sys
import ConfigParser
try: import json #python 2.6 included simplejson as json
except ImportError: import simplejson as json

def doPost(configFile, uri, paramMap):
    config = ConfigParser.RawConfigParser()
    config.read(configFile)

    HOST = config.get("Connection", "host")
    APIKEY = config.get("Connection", "apikey")

    headers = {"x-addama-apikey": APIKEY, "Content-type": "application/x-www-form-urlencoded", "Accept": "text/plain" }

    print("POST https://" + HOST + uri)

    params = "empty"
    if paramMap:
        params = urllib.urlencode(paramMap)

    print "parameters: [" + params + "]"

    conn = httplib.HTTPSConnection(HOST)
    conn.request("POST", uri, params, headers)

    resp = conn.getresponse()
    if resp.status == 200:
        output = resp.read()
        try:
            print json.dumps(json.JSONDecoder().decode(output), sort_keys=True, indent=4)
        except:
            print output
    else:
        print resp.status, resp.reason

    conn.close()

if __name__ == "__main__":
    numberOfArgs = len(sys.argv)

    if (numberOfArgs < 3):
        print "Usage"
        print "   python post_gae.py <configFile> <uri> [param1=value1 ... paramN=valueN]"
        print "      configFile -> apikey file containing secret key and host URL for an Addama registry instance on GAE"
        print "             obtain from (https://my-appspot.appspot.com/addama/apikeys/file)"
        print "      uri -> location of service or resource within Addama registry"
        print "      paramX=valueX -> (optional) pairs of POST parameters to be sent in request"
        sys.exit(0)

    paramMap={}
    paramList=[]
    if (numberOfArgs > 3):
        paramList = sys.argv[3:]

    print "passing parameters:", paramList
    for index in range(0, len(paramList), 1):
        parameter = paramList[index]
        eqpos = parameter.find("=")
        key = parameter[0:eqpos]
        value = parameter[eqpos+1:]
        paramMap[key] = value

    doPost(sys.argv[1], sys.argv[2], paramMap)
