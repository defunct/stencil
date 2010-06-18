package com.goodworkalan.stencil;

/**
 * A document type definition in an XML document.
 *
 * @author Alan Gutierrez
 */
public class DocumentTypeDefinition {
	/** Whether this is the start of a DTD. */
	public final boolean start;

	/** The name. */
	public final String name;
	
	/** The public id. */
	public final String publicId;
	
	/** The system id. */
	public final String systemId;

	/**
	 * Create a document type definition with the given name, public id and
	 * system id.
	 * 
	 * @param start
	 *            Whether this is the start of a DTD.
	 * @param name
	 *            The name.
	 * @param publicId
	 *            The public id.
	 * @param systemId
	 *            The system id.
	 */
	public DocumentTypeDefinition(boolean start, String name, String publicId, String systemId) {
		this.start = start;
		this.name = name;
		this.publicId = publicId;
		this.systemId = systemId;
	}
}
