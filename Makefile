CHIBI_IMAGES = gfx/images/icons.png gfx/images/smallicons.gif gfx/images/textures32.png
CHIBI_BIN = bin/chibipaint/ bin/chibitest/

JAVA_14_PATH = C:\Program Files (x86)\Java\j2re1.4.2_19

all: splash.jar chibi.jar chibipaint.jar

splash.jar: splash.out.jar
#	mv splash.out.jar splash.jar
	java -jar "retrotranslator/retrotranslator-transformer-1.2.9.jar" -srcjar splash.out.jar -destjar splash.jar -classpath "${JAVA_14_PATH}\lib\rt.jar;${JAVA_14_PATH}\lib\jce.jar;${JAVA_14_PATH}\lib\jsse.jar;${JAVA_14_PATH}\javaws\javaws.jar;bootstrap.jar" 

chibi.jar: chibi.out.jar
#	mv chibi.out.jar chibi.jar
	java -jar "retrotranslator/retrotranslator-transformer-1.2.9.jar" -srcjar chibi.out.jar -destjar chibi.jar -classpath "${JAVA_14_PATH}\lib\rt.jar;${JAVA_14_PATH}\lib\jce.jar;${JAVA_14_PATH}\lib\jsse.jar;${JAVA_14_PATH}\javaws\javaws.jar;bootstrap.jar" 

chibipaint.jar: chibipaint.out.jar
	java -jar "retrotranslator/retrotranslator-transformer-1.2.9.jar" -srcjar chibipaint.out.jar -destjar chibipaint.jar -classpath "${JAVA_14_PATH}\lib\rt.jar;${JAVA_14_PATH}\lib\jce.jar;${JAVA_14_PATH}\lib\jsse.jar" 

splash.out.jar: bootstrap.jar bin/splash/*
	jar -cf splash.in.jar -C bin/ splash/
#	mv splash.in.jar splash.out.jar
	java -jar proguard/lib/proguard.jar @ splash.pro -verbose
	
chibi.out.jar: bootstrap.jar ${CHIBI_IMAGES} ${CHIBI_BIN}
	jar -cf chibi.in.jar -C bin/ chibipaint/
	jar -uf chibi.in.jar -C bin/ images/
#	mv chibi.in.jar chibi.out.jar
	java -jar proguard/lib/proguard.jar @ chibi.pro -verbose

chibipaint.out.jar: ${CHIBI_IMAGES} ${CHIBI_BIN}
	jar -cf chibipaint.in.jar -C bin/ chibipaint/
	jar -uf chibipaint.in.jar -C bin/ images/
	jar -uf chibipaint.in.jar -C bin/ splash/
	jar -uf chibipaint.in.jar -C bin/ bootstrap/
	jar -uf chibipaint.in.jar -C bin/ javax/
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
	rm -f chibipaint.jar
	rm -f chibipaint.in.jar
	rm -f chibipaint.out.jar
	rm -f chibi.jar.pack.gz
	rm -f splash.jar.pack.gz
	rm -f chibipaint.jar.pack.gz

%.jar.pack.gz : %.jar
	pack200 -E9 $@ $<
	
install: chibi.jar chibi.jar.pack.gz splash.jar splash.jar.pack.gz chibipaint.jar chibipaint.jar.pack.gz
	cp chibi.jar ../trunk/public_html/oekaki/chibi/
	cp splash.jar ../trunk/public_html/oekaki/chibi/
	cp chibipaint.jar ../trunk/public_html/oekaki/chibi/	
	cp chibi.jar.pack.gz ../trunk/public_html/oekaki/chibi/
	cp splash.jar.pack.gz ../trunk/public_html/oekaki/chibi/
	cp chibipaint.jar.pack.gz ../trunk/public_html/oekaki/chibi/	
