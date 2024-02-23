class InvalidRegisterRequest(ValueError):
    """ If we receive an invalid register request (e.g, invalid parameters) """


class InvalidDeleteRequest(ValueError):
    """ If we receive an invalid delete request (e.g, invalid parameters, unexistant entry) """


class InvalidServiceEntry(ValueError):
    """ Badly formatted service thrown by ServiceEntry dataclass """