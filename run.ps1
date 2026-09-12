Push-Location "$PSScriptRoot/run"

java `
	-Xms1G -Xmx1G `
	-XX:+AllowEnhancedClassRedefinition `
	-XX:+UseG1GC `
	-XX:+UseCompactObjectHeaders `
	-XX:+UseStringDeduplication `
	-jar server.jar

Pop-Location