package com.techmorshed.drinkshop.Utils;

import com.techmorshed.drinkshop.retrofit.IDrinkShopAPI;
import com.techmorshed.drinkshop.retrofit.RetrofitClient;

public class Common {
    private static  final String BASE_URL = "http://192.168.0.103/drinkshop/";

    public static IDrinkShopAPI getApi()
    {
        return RetrofitClient.getClient(BASE_URL).create(IDrinkShopAPI.class);

    }

}
