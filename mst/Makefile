all:
	jar cvf MST.jar *.class

clean:
	@if test -f $(MST.jar); then \
      rm MST.jar; \
      echo Removed file MST.jar; \
    else \
      echo Nothing to clean up, file does not exist.; \
    fi
 
up: clean all
	p2pmpirun -l xferfiles -n 8 MST