VER = $(shell lein pprint :version)

package:	
	lein clean
	lein uberjar
	rsync -av pkg/ monocle-$(VER)
	mkdir -p monocle-$(VER)/lib
	cp target/*-standalone.jar monocle-$(VER)/lib/monocle.jar
	tar cf - monocle-$(VER) | gzip >monocle-$(VER).tar.gz

