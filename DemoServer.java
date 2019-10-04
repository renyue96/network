import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class DemoServer extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	BorderPane bp;
	TextArea messages;
	TextField messageWritingField;
	Button sendButton;

	Socket clientConnection;
	ObjectOutputStream output;
	ObjectInputStream input;

	int number;

	@Override
	public void start(Stage primaryStage) throws Exception {

		bp = new BorderPane();

		messages = new TextArea();
		messages.setEditable(false);
		bp.setCenter(messages);

		HBox messageWritingArea = new HBox();
		messageWritingField = new TextField();
		sendButton = new Button("send");
		messageWritingArea.getChildren().add(messageWritingField);
		messageWritingArea.getChildren().add(sendButton);
		bp.setBottom(messageWritingArea);

		primaryStage.setTitle("JavaFX Server");
		primaryStage.setResizable(false);
		primaryStage.setScene(new Scene(bp, 400, 300));
		primaryStage.show();

		/* set up button handler */
		sendButton.setOnAction((event) -> {
			try {
				sendMessage(messageWritingField.getText());
				messageWritingField.setText("");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		/* set up networking */
		ServerSocket serverSocket = new ServerSocket(4000);
		appendMessage("Waiting for a client connection...");

		// create a new thread to manage connections, so it doesn't block the UI
		Thread socketAccepter = new Thread() {
			public void run() {
				try {
					// accept connection
					clientConnection = serverSocket.accept();
					output = new ObjectOutputStream(clientConnection.getOutputStream());
					input = new ObjectInputStream(clientConnection.getInputStream());

					Platform.runLater(() -> {
						appendMessage("connected!");
					});

					// read in messages continuously
					try {
						while (true) {
							DemoMessage received = (DemoMessage) input.readObject();
							Platform.runLater(() -> appendMessage("[CLIENT]: " + received.getMessage()));
						}
					} catch (SocketTimeoutException exc) {
						Platform.runLater(() -> appendMessage("Timed out while waiting for a response."));
					} catch (EOFException exc) {
						Platform.runLater(() -> appendMessage("Received end of stream from client."));
					} catch (IOException exc) {
						// some other I/O error: print it, log it, etc.
						exc.printStackTrace();
					} catch (ClassNotFoundException exc) {
						exc.printStackTrace();
					} finally {
						clientConnection.close();
						serverSocket.close();
					}
				} catch (IOException ioe) {
					ioe.printStackTrace();
					Platform.runLater(() -> appendMessage("An error occured while waiting for a connection."));
				}
			}
		};
		socketAccepter.start();
	}

	/**
	 * Writes a DemoMessage object to the client and updates the messages TextArea
	 * via appendMessage.
	 * 
	 * @param message the message to send to the server
	 * @throws IOException
	 */
	public void sendMessage(String message) throws IOException {
		output.writeObject(new DemoMessage(message, number++));
		appendMessage("[SERVER]: " + message);
	}

	/**
	 * Appends the given message to the end of the messages TextArea. It will
	 * automatically append a newline to the end of the given message.
	 * 
	 * @param message the message to be appended to the messages TextArea
	 */
	private void appendMessage(String message) {
		messages.setText(messages.getText() + message + '\n');
	}
}
