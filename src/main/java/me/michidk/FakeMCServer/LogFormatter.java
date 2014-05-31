package me.michidk.FakeMCServer;

import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter
{
	private SimpleDateFormat date = new SimpleDateFormat("HH:mm:ss");
	
	public String format(LogRecord logRecord)
	{
		StringBuilder buffer = new StringBuilder( );
		
		buffer.append(date.format(Long.valueOf(logRecord.getMillis())));
		buffer.append(" [" + logRecord.getLevel( ) + "] ");
		buffer.append( logRecord.getMessage( ) + '\n' );
		
		return buffer.toString( );
	}
}
