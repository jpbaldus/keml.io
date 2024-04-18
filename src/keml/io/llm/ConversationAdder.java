package keml.io.llm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Collectors;

import keml.Conversation;
import keml.MessageExecution;
import keml.ReceiveMessage;
import keml.SendMessage;

public class ConversationAdder {
	
	public static void addOriginalConv(Conversation conv, File originalConv) throws IOException {
		ArrayList<LLMMessage> msgs = LLMMessage.readFile(Path.of(originalConv.getPath()));
		ArrayList<MessageExecution> kemlMsgs = conv.getAuthor().getMessageExecutions().stream()
				.filter(msg -> msg.getCounterPart().getName().equals("LLM")).collect(Collectors.toCollection(ArrayList::new));
		if (msgs.size() != kemlMsgs.size()) {
			System.err.println("The size of original messages ("+ msgs.size() + ") and keml messages with LLM ("+ kemlMsgs.size() + ") do not fit.");
		}
		for(int i=0; i<msgs.size(); i++) {
			MessageExecution kemlM = kemlMsgs.get(i);
			LLMMessage msg = msgs.get(i);
			if(kemlM instanceof SendMessage && msg.getAuthor().equals("Author") ||
					kemlM instanceof ReceiveMessage && msg.getAuthor().equals("LLM"))
				kemlM.setOriginalContent(msg.getMessage());
			else
				System.err.println("No match on "+msg +" and " + kemlM);
		}
	}

}
