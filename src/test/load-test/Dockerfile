FROM blazemeter/taurus

RUN pip install pandas && pip install matplotlib

ADD entrypoint.sh /entrypoint.sh
ADD summary.py /summary.py
RUN mkdir /minimal_outputs

ADD main.yaml /test/main.yaml
ADD taurus/ /test/taurus/
ADD inputs/ /test/inputs/
ADD jmeter/ /test/jmeter/

ENTRYPOINT ["sh", "/entrypoint.sh" ]
