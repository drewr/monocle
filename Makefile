VER = $(shell lein pprint :version)

package:	
	lein clean
	lein uberjar
	rsync -av pkg/ target/monocle-$(VER)
	mkdir -p target/monocle-$(VER)/lib
	cp target/*-standalone.jar target/monocle-$(VER)/lib/monocle.jar
	cd target; tar cf - monocle-$(VER) | gzip >monocle-$(VER).tar.gz

