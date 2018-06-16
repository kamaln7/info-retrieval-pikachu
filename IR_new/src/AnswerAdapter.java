import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class AnswerAdapter extends TypeAdapter<Answer> {
	@Override
	public void write(JsonWriter writer, Answer a) throws IOException {
		writer.beginObject();

		writer.name("answer").value(a.answer);
		writer.name("score").value(a.score.toString());
		writer.endObject();
	}

	@Override
	public Answer read(JsonReader arg0) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
}
