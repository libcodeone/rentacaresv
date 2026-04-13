-- Ampliar el ENUM photo_type en rental_photo para incluir los tipos de documentos
ALTER TABLE rental_photo
    MODIFY COLUMN photo_type ENUM(
        'DELIVERY_EXTERIOR',
        'DELIVERY_INTERIOR',
        'DELIVERY_ACCESSORIES',
        'RETURN_EXTERIOR',
        'RETURN_INTERIOR',
        'RETURN_ACCESSORIES',
        'DOCUMENT_ID_FRONT',
        'DOCUMENT_ID_BACK',
        'DOCUMENT_LICENSE_FRONT',
        'DOCUMENT_LICENSE_BACK'
    ) NOT NULL;
