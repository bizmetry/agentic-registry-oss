#!/bin/sh
set -e

echo "======================================"
echo " Starting Agent Registry Backend"
echo "======================================"
echo "Java version:"
java -version
echo ""
echo "Using jar: /app/app.jar"
echo ""

# JVM options (podés extenderlas luego)
JAVA_OPTS=${JAVA_OPTS:-"-Xms256m -Xmx1g"}

echo "JAVA_OPTS=$JAVA_OPTS"
echo ""

exec java $JAVA_OPTS -jar /app/app.jar

