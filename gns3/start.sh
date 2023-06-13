#!/bin/sh
if [ "${CONFIG}x" == "x" ]; then
	CONFIG=/data/config.ini
fi

if [ ! -e $CONFIG ]; then
	cp /config.ini /data
fi

# virbr0 used by GNS3 for NAT object
brctl addbr virbr0
ip link set dev virbr0 up
if [ "${BRIDGE_ADDRESS}x" == "x" ]; then
  BRIDGE_ADDRESS=172.21.1.1/24
fi
ip ad add ${BRIDGE_ADDRESS} dev virbr0
iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
dnsmasq -i virbr0 -z -h --dhcp-range=172.21.1.10,172.21.1.250,4h

# start docker
dockerd --storage-driver=vfs --data-root=/data/docker/ &

# start gns3server
gns3server -A --config /data/config.ini