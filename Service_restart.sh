# vi /opt/scripts/service-monitor.sh

#!/bin/bash
serv=httpd
sstat=stopped
service $serv status | grep -i 'running\|stopped' | awk '{print $3}' | while read output;
do
echo $output
if [ "$output" == "$sstat" ]; then
    service $serv start
    echo "$serv service is UP now.!" | mail -s "$serv service is DOWN and restarted now On $(hostname)" daygeek@gmail.com
    else
    echo "$serv service is running"
    fi
done
