import {REGEX} from "@waterfun/web-core/src/regex";
import {translate} from "@/utils/translator";

const keyPrefix = 'auth.validate.'

export const validateAuthname = (loginType: string) =>{
    return(rule:any,value:any,callback:any)=>{
        if(value === ''){
            callback(new Error(translate(keyPrefix + 'usernameEmpty')));
            return;
        }
        switch(loginType){
            case 'email':
                if(! REGEX.email.test(value)){
                    callback(new Error(translate(keyPrefix + 'invalidEmail')));
                }break;
            case 'sms':
                if(! REGEX.phone.test(value)){
                    callback(new Error(translate(keyPrefix + 'invalidPhone')));
                }break;
            case 'password':
                if(value.length < 4 || value.length > 20){
                    callback(new Error(translate(keyPrefix + 'usernameOutOfLength')));
                }else if(! REGEX.username.test(value)){
                    callback(new Error(translate(keyPrefix + 'invalidUsername')));
                }break;
            default: callback();
        }
        callback();
    }
}

export const validateUsername = (allowEmpty?:boolean) =>{
    return(rule:any,value:any,callback:any)=>{
        if(!value && !allowEmpty){
            callback(new Error(translate(keyPrefix + 'usernameEmpty')));
            return;
        }
        if(allowEmpty){
            callback();
            return;
        }
        if(value.length < 4 || value.length > 20){
            callback(new Error(translate(keyPrefix + 'usernameOutOfLength')));
        }
        if(! REGEX.username.test(value)){
            callback(new Error(translate(keyPrefix + 'invalidUsername')));
        }
    }
}

export const validateEmail = (allowEmpty?:boolean) =>{
    return createFieldValidator({
        regex: REGEX.email,
        emptyErrorKey: 'emailEmpty',
        invalidErrorKey: 'invalidEmail',
        allowEmpty: allowEmpty,
    })
}

export const validatePhoneNumber = (allowEmpty?:boolean) =>{
    return createFieldValidator({
        regex: REGEX.phone,
        emptyErrorKey: 'phoneEmpty',
        invalidErrorKey: 'invalidPhone',
        allowEmpty: allowEmpty,
    })
}

export const createFieldEmptyValidator = (fieldName:string,allowEmpty:boolean) => {
    return(rule:any,value:any,callback:any)=>{
        if(!value && !allowEmpty){
            callback(new Error(translate(`${keyPrefix}${fieldName}Empty`)));
        }else{
            callback();
        }
    }
}

export const createFieldValidator = (
    options: {
        regex?: RegExp,
        emptyErrorKey: string,
        invalidErrorKey?: string,
        allowEmpty?: boolean,
        preCheck?: (value: string) => boolean
    }
) => {
    return (_: any, value: any, callback: any) => {
        if (!value && !options.allowEmpty) {
            callback(new Error(translate(keyPrefix + options.emptyErrorKey)));
            return;
        }

        if (options.allowEmpty && !value) {
            callback();
            return;
        }

        if (options.preCheck && !options.preCheck(value)) {
            callback(new Error(translate('customPrecheckError')));
            return;
        }

        if (options.regex && options.invalidErrorKey &&!options.regex.test(value)) {
            callback(new Error(translate(keyPrefix + options.invalidErrorKey)));
            return;
        }

        callback();
    };
};

export const validateVerifyCode = (allowEmpty:boolean)=> createFieldEmptyValidator('verifyCode',allowEmpty);

export const validatePassword = (allowEmpty?: boolean) => {
    return (rule: any, value: any, callback: any) => {
        if (!value && !allowEmpty) {
            callback(new Error(translate(keyPrefix + 'passwordEmpty')));
            return;
        }

        if (allowEmpty && !value) {
            callback();
            return;
        }

        if (value.length <= 8) {
            callback(new Error(translate(keyPrefix + 'passwordTooShort')));
            return;
        }

        if (!/[a-z]/.test(value) || !/[A-Z]/.test(value) || !/[0-9]/.test(value)) {
            callback(new Error(translate(keyPrefix + 'passwordInvalid')));
            return;
        }

        callback();
    };
};