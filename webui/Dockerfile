FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY . /opt/webui/

EXPOSE 8080

ENTRYPOINT ["nginx","-g","daemon off;"]