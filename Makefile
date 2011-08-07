CHIBI_IMAGES = gfx/images/icons.png gfx/images/smallicons.gif gfx/images/textures32.png
CHIBI_BIN = bin/chibipaint/ bin/chibitest/

all: splash.jar chibi.jar

splash.jar: splash.out.jar
	mv splash.out.jar splash.jar
#	java -jar "retrotranslator/retrotranslator-transformer-1.2.9.jar" -srcjar splash.out.jar -destjar splash.jar

chibi.jar: chibi.out.jar
	mv chibi.out.jar chibi.jar
#	java -jar "retrotranslator/retrotranslator-transformer-1.2.9.jar" -srcjar chibi.out.jar -destjar chibi.jar

splash.out.jar: bootstrap.jar bin/splash/*
	jar -cf splash.in.jar -C bin/ splash/
	java -jar proguard/lib/proguard.jar @ chibisplash.pro -verbose
	
chibi.out.jar: bootstrap.jar ${CHIBI_IMAGES} ${CHIBI_BIN}
	jar -cf chibi.in.jar -C bin/ chibipaint/
	jar -uf chibi.in.jar -C bin/ images/
	java -jar proguard/lib/proguard.jar @ chibipaint.pro -verbose

bootstrap.jar: bin/bootstrap/*
	jar -cf bootstrap.jar -C bin/ bootstrap/

clean:
	rm -f splash.jar
	rm -f splash.in.jar
	rm -f splash.out.jar
	rm -f chibi.jar
	rm -f chibi.in.jar
	rm -f chibi.out.jar

install:
	cp chibi.jar ../trunk/public_html/oekaki/chibi/
	cp splash.jar ../trunk/public_html/oekaki/chibi/