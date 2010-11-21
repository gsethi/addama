#!/usr/bin/env python2.6

# For each recently modified file within the directory tree, an item
# is inserted into the Addama RSS feed for this file repository
# resource.
#
# The intent of the script below is that it is run via a cron job
# every 15 minutes looking for files modified within the past hour.
# It is run in these overlapping windows of time to be tolerant of
# temporary failures (e.g., if the Google App Engine app times out).
# Note that a key is added to the message so that repeated POSTs for
# the same file will be a single entry in the feed.

import os, datetime, pwd, urllib, httplib, ConfigParser

# filepath to Addama python config file
# e.g. ~/.ssh/addama.config
CONFIG_FILE = os.sys.argv[1]
# root of the directory tree we want to monitor
# e.g. /var/ftproot/incoming
TREE_ROOT = os.sys.argv[2] # '/Users/deflaux/bin'
# the feed URL to which we want to POST items
# e.g. /addama/feeds/sageFTPfeed
FEED_URL = os.sys.argv[3]

DEBUG = True
NOW = datetime.datetime.today()
ONE_DAY_AGO = datetime.timedelta(days=1)

config = ConfigParser.RawConfigParser()
config.read(CONFIG_FILE)  
HOST = config.get("Connection", "host")
API_KEY = config.get("Connection", "apikey")
HEADERS = {"x-addama-apikey": API_KEY,
           "Content-type": "application/x-www-form-urlencoded",
           "Accept": "text/plain" }

for root, dirs, files in os.walk(TREE_ROOT):
    for fileName in files:
        # Get the name, user, and last mod date for each file in our tree
        fullName = os.path.join(root, fileName)
        try:
            fileInfo = os.stat(fullName)
        except OSError, err:
            print(err)
            continue
        user = pwd.getpwuid(fileInfo.st_uid).pw_name
        lastModDate = datetime.datetime.fromtimestamp(fileInfo.st_mtime)

        # Skip files not recently modified
        timeSinceMod = NOW - lastModDate
        if(ONE_DAY_AGO < timeSinceMod):
            continue

        # Formulate a message about this recently updated file and
        # POST it to the Addama RSS feed
        msg = '{"key":"' + fullName # for idempotent additions
        msg += '", "text":"' + fullName
        msg += '", "author":"' + user
        msg += '", "date":"' + lastModDate.isoformat() + '"}'
        if(DEBUG):
            print("About to send: " + msg)
        paramMap = {'item' : msg}
        params = urllib.urlencode(paramMap)
        # If we were worried about performance we might reuse the
        # connection instead of reopening it for each iteration of the
        # loop, but this is more fault tolerant
        conn = httplib.HTTPSConnection(HOST, timeout=30)
        if(DEBUG):
            conn.set_debuglevel(2);
        try:
            conn.request("POST", FEED_URL, params, HEADERS)
            resp = conn.getresponse()
            if resp.status == 200 and DEBUG:
                output = resp.read()
                print output
            else:
                print resp.status, resp.reason
        except Exception, err:
            print(err)
            continue
        conn.close()
