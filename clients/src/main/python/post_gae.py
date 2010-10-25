import socket
import httplib
import urllib
import sys
import ConfigParser
try: import json #python 2.6 included simplejson as json
except ImportError: import simplejson as json

def doPost(configFile, uri, param):
    config = ConfigParser.RawConfigParser()
    config.read(configFile)

    HOST = config.get("Connection", "host")
    APIKEY = config.get("Connection", "apikey")

    headers = {"x-addama-apikey": APIKEY,"Content-type": "application/x-www-form-urlencoded", "Accept": "text/plain" }
    params = urllib.urlencode(param)

    print("POST https://" + HOST + uri)
    print "params: " + params

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
    if (len(sys.argv) < 3):
	print ""
        print "python post.py <configFile> <uri> param1 value1 ... paramN valueN"
        print "where configFile defines an apikey and host URL for a GAE appspot"
        print "      uri defines a URI to the service/directory"
        print "      valueX paramX are a pair of POST parameters to be sent"
	sys.exit(0)
    paramMap={}
    paramList=[];
    if (len(sys.argv) > 4):
	paramList = sys.argv[3:]
    print "param size: ", len(paramList)
    for index in range(0,len(paramList),2):
	print "index: ", index
	print "name: " + paramList[index]
	print "value: " + paramList[index+1]
	paramMap[paramList[index]] = paramList[index+1]
    doPost(sys.argv[1],sys.argv[2],paramMap)
