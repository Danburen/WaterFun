import {PromiseApiRes} from "@waterfun/web-core/src/types";
import {Page} from "~/types";
import request from "../utils/axiosRequest"
import {AxiosResponse} from "axios";
export interface ShipResponse {

}



export const listShips = () : Promise<Page<ShipResponse>> => {
    return request.get<Page<ShipResponse>>("/ships/listShips");
}