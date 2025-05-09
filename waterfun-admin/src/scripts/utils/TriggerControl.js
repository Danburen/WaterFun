/**
 * Throttle execution of a function
 * must be closure used. use like const func = throttle(...)
 * @param func function to execute
 * @param delay delay between two function execute
 * @returns {(function(...[*]): void)|*}
 */
export function throttle(func, delay) {
    let canRun = true;
    return function (...args) {
        if(!canRun) return;
        canRun = false;
        func.apply(this,args);
        setTimeout(()=> canRun = true ,delay);
    };
}

/**
 * Debounce execution of a function
 * must be closure used. use like const func = debounce(...)
 * @param func function to execute
 * @param delay delay between two function execute
 * @returns {(function(...[*]): void)|*}
 */
export function debounce(func, delay) {
    let timer;
    return function (...args) {
        clearTimeout(timer);
        timer = setTimeout(()=>{
            func.apply(this,args);
        }, delay);
    };
}
