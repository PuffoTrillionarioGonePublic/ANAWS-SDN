FROM docker:dind

RUN apk add --no-cache --virtual=build-dependencies jq gcc python3-dev musl-dev linux-headers
RUN apk add --no-cache \
	cpulimit=0.2-r2 \
	dnsmasq=2.89-r5 \
	docker=23.0.6-r2 \
	dynamips=0.2.23-r1 \
	libcap=2.69-r0 \
	py3-pip=23.1.2-r0 \
	qemu-img=8.0.2-r0 \
	qemu-system-x86_64=8.0.2-r0 \
	ubridge=0.9.18-r1 \
	util-linux=2.38.1-r8 \
	vpcs=0.8.2-r1

RUN pip install --no-cache-dir gns3-server==2.2.40
RUN apk del --purge build-dependencies

COPY ./start.sh /start.sh
COPY ./config.ini /config.ini

WORKDIR /data
VOLUME ["/data"]

CMD [ "/start.sh" ]