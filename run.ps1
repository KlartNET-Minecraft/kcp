Push-Location "$PSScriptRoot/run"

java `
	-Xms2G -Xmx2G `
	-XX:+UseG1GC `
	-XX:+UseCompactObjectHeaders `
	-XX:+UseStringDeduplication `
	-jar server.jar

Pop-Location