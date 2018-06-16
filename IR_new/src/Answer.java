import com.google.gson.annotations.JsonAdapter;

@JsonAdapter(AnswerAdapter.class)
public class Answer {

	public String answer;
	public Float score;
	public transient Integer doc;

}
