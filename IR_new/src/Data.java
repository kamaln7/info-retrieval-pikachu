import java.util.List;

public class Data {
	
	private String main_category;
	private String question;
	private List<String> nbestanswers;
	private String answer;
	private String id;

	public String getMain_category() {
		return main_category;
	}
	
	public String getQuestion() {
		return question;
	}
	
	public List<String> getNbestanswers() {
		return nbestanswers;
	}
	
	public String getAnswer() {
		return answer;
	}
	
	public String getId() {
		return id;
	}
	
	public void setMain_category(String _main_category) {
		main_category = _main_category;
	}
	
	public void setQuestion(String _question) {
		question = _question;
	}
	
	public void setNbestanswers(List<String> _nbestanswers) {
		nbestanswers = _nbestanswers;
	}
	
	public void setAnswer(String _answer) {
		answer = _answer;
	}
	
	public void setId(String _id) {
		id = _id;
	}

}
