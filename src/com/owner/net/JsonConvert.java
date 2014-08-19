package com.owner.net;

import java.util.List;

import com.owner.constant.Constants;
import com.owner.entity.Data;

public class JsonConvert {
   public static Data convertStringToData(String resultString)
   {
	   Data data=Constants.gson.fromJson(resultString, Data.class);
	   if(data!=null)
	   {
		   return data;
	   }
	   return null;
   }
}
