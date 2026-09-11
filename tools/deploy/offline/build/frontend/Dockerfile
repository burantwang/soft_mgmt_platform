# ============================================================
# 前端运行时镜像（离线部署专用）
# dist 在构建机打好后 COPY 进来，镜像内不装 Node 不编译
# ============================================================
FROM nginx:alpine

COPY dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
