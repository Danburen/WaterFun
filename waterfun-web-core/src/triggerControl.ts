/**
 * Throttle execution of a function
 * @param func function to be throttled
 * @param delay delay between two function execution
 * @param onThrottled optional callback when function is throttled
 */
export function throttle(func: Function, delay: number, onThrottled?: Function) {
    let canRun = true;
    return (...args: any[]) => {
        if(canRun) {
            func.apply(func, args);
            canRun = false;
            setTimeout(()=> canRun = true, delay);
        } else if (onThrottled) {
            onThrottled.apply(onThrottled, args);
        }
    }
}

/**
 * Debounce execution of a function
 * Only apply at the last trigger
 * @param func function to be debounced executing
 * @param delay delay debounce
 */
export function deBounce(func: Function,delay: number) {
    let timer:number;
    return(...args: any[]) => {
        clearTimeout(timer);
        timer = setTimeout(() => {
            func.apply(func, args);
        },delay);
    }
}

/**
 * Debounce execution of a function
 * Only apply at the first time
 * @param func function to be debounced executing
 * @param delay delay debounce
 */
export function deBounceLeading(func: Function,delay: number) {
    let timer: number | null;
    return(...args: any[]) => {
        if(!timer) func.apply(func, args);
        clearTimeout(timer as number);
        timer = setTimeout(() => {
            timer = null;
        },delay);
    }
}