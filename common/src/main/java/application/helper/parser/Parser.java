package application.helper.parser;

//import applications.state_engine.utils.Configuration;

import application.util.Configuration;
import application.util.datatypes.StreamValues;

import java.io.Serializable;
import java.util.List;

public abstract class Parser<T> implements Serializable {
	private static final long serialVersionUID = -1221926672447206098L;
	protected Configuration config;

    public void initialize(Configuration config) {
        this.config = config;
    }

    public abstract T parse(char[] str);

	public abstract List<StreamValues> parse(String value);


	//public abstract List<StreamValues> parse(String[] input);
}