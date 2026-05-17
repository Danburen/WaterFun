import Schema from 'async-validator';

Schema.warning = function () {};

export default defineNuxtPlugin(() => {
    return {
        provide: {
            asyncValidate: Schema
        }
    };
});