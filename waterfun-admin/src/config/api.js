const API_CONFIG = {
    development:{
        baseURL : "http://localhost:8080/api"
    },
    production: {
        baseURL: 'https://api.your-domain.com', // 生产环境
    },
    // 可扩展其他环境（如测试环境）
    staging: {
        baseURL: 'https://staging-api.your-domain.com',
    }
}

const env = process.env.NODE_ENV || 'development';
export default  API_CONFIG[env].baseURL;