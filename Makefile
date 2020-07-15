docker:
	docker build --tag opentest  -f Dockerfile .

run:
	docker-compose down -v
	./gradlew assemble installDist
	docker-compose up --build