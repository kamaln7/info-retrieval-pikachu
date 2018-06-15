import java.util.ArrayList;

import com.google.gson.annotations.SerializedName;

public class nfL6Entry {
	@SerializedName("main_category")
	public String MainCategory;
	@SerializedName("question")
	public String Question;
	@SerializedName("answer")
	public String Answer;
	@SerializedName("id")
	public String ID;
	@SerializedName("nbestanswers")
	public ArrayList<String> NBestAnswers;

	public nfL6Entry() {
		this.NBestAnswers = new ArrayList<String>();
	}
}
